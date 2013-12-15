(ns ecregister.core
  (:gen-class)
  (:use [seesaw.core])
  (:use [seesaw.mig])
  (:use [seesaw.widgets.log-window])
  (:require [clojure.string :refer [blank?]])
  (:require [clojure.core.async :refer [chan alts!! timeout thread]])
  )
; (require '[clojure.core.async :refer [chan alts!! alts! timeout thread put! go-loop]])
; (require '[clojure.string :refer [blank?]])

(defn build-avatars-tab []
  (let [bg-stamp-pos (button-group)
        form (mig-panel
              :items [[(label "Имя пользователя:") ""]
                      [(text :id :username :columns 15) "wrap"]
                      [(label "Положение штампа:")]
                      [(flow-panel :items [(radio :id :top :text "Сверху" :group bg-stamp-pos)
                                           (radio :id :bottom :text "Снизу" :group bg-stamp-pos)]) "wrap"]
                      [(button :text "Проштамповать") "wrap"]
                      [(scrollable (log-window :id :log
                                               :rows 8
                                               :columns 80
                                               ))
                       "span"]])]
    (let [te-name (select form [:#username])
          lb-log (select form [:#log])
          c (chan)]
      (listen te-name
              :document (fn [e]
                          (put! c (text te-name)))
              :focus-gained (fn [e]
                             (go-loop []
                                      (let [[v ch] (alts! [c (timeout 1000)])]
                                        (if (not= v "stop")
                                          (do
                                            (if (not (blank? v))
                                              (log lb-log (str "read " v ", waiting continues...\n"))
                                              (log lb-log (str "timeout reached, getting avatar for " (text te-name) "\n")))
                                            (recur))
                                          (log lb-log "exiting loop.")))))
              :focus-lost (fn [e]
                            (log lb-log "sending stop\n")
                            (put! c "stop")))

      )
      form
      )
    )
;; (config! f :content (build-content))
;; (def f (make-frame (build-content)))
;; (show! f)

(defn build-posts-tab []
  (label "Posts"))

(defn build-content []
  (horizontal-panel
   :items [(tabbed-panel
            :placement :bottom
            :tabs [ {:title "Аватарки" :content (build-avatars-tab)}
                    {:title "Посты" :content (build-posts-tab)}])
           ]))

(defn make-frame [content]
  (println "content " content)
  (frame
   :title "Free Away Admin"
   :on-close :hide
   :size [640 :by 480]
   :content (label "hello") ;; TODO replace label => (build-content)
   ))

(defn -main [& args]
  (invoke-later
   (->
    (build-content)
    (make-frame)
;;    pack!
    show!
    ))
)
