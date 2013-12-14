(ns ecregister.core
  (:gen-class)
  (:use [seesaw.core])
  (:use [seesaw.mig])
  (:require [clojure.core.async :refer [chan alts!! timeout thread]])
  )
(require '[clojure.core.async :refer [chan alts!! timeout thread put!]]
 )

(defn build-avatars-tab []
  (let [bg-stamp-pos (button-group)
        form (mig-panel
              :items [[(label "Имя пользователя:") ""]
                      [(text :id :username :columns 15) "wrap"]
                      [(label "Положение штампа:")]
                      [(flow-panel :items [(radio :id :top :text "Сверху" :group bg-stamp-pos)
                                           (radio :id :bottom :text "Снизу" :group bg-stamp-pos)]) "wrap"]
                      [(button :text "Проштамповать") ""]
                      [(label :id :log :text "Some log") ""]])]
    (let [te-name (select form [:#username])
          lb-log (select form [:#log])
          c (chan)]
      (listen te-name :document (fn [e]
                                  (config! lb-log :text "starting wait...")
                                  (thread (let [[v ch] (alts!! [c (timeout 1000)])]
                                            (config! lb-log :text (str "wait ended. read " v))))
                                  )))
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
