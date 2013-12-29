(ns ecregister.core
  (:gen-class)
  (:use [seesaw.core])
  (:use [seesaw.mig])
  (:use [seesaw.border])
  (:use [seesaw.widgets.log-window])
  (:require [clojure.string :refer [blank?]])
  (:require [clojure.core.async :refer [chan >!! <!! <! >! alts!! alts! timeout thread put! go go-loop close!]])
  (:require [ecregister.avatars :as av])
  )
(require '[ecregister.avatars :as av])
(use '[seesaw.border])

;;(require '[clojure.core.async :refer [chan >!! <!! <! >! alts!! alts! timeout thread put! go go-loop close!]])
(defn retrieve-avatar [username lb-log]
  (when (not (blank? username))
    (log lb-log (str "retrieving avatar for " username "\n"))
    (go
     (let [c (chan)
           resp (<! (av/get-avatar-url username c))]
       (log lb-log (str "received response " resp "\n"))))))

(defn build-avatars-tab []
  (let [bg-stamp-pos (button-group)
        form (mig-panel
              :items [[(label "Имя пользователя:") ""]
                      [(text :id :username :columns 15)]
                      [(label :text "orig"
                              :border (line-border :color "#ddd" :thickness 1)
                              :halign :center)
                       "span 1 3,w 100px::, h 100px::,wrap,top"]
                      [(label "Положение штампа:")]
                      [(flow-panel :items [(radio :id :top :text "Сверху" :group bg-stamp-pos)
                                           (radio :id :bottom :text "Снизу" :group bg-stamp-pos)]) "wrap"]
                      [(button :text "Проштамповать") "wrap,skip 1"]
                      [(label "") "grow,push,span 2"] ;; empty filler
                      [(label :text "stamped"
                              :border (line-border :color "#ddd" :thickness 1)
                              :halign :center)
                       "w 100px::, h 100px::,wrap,top,gaptop 10px"]
                      [(scrollable (log-window :id :log
                                               :rows 8
                                               :columns 80
                                               ))
                       "span,growx"]]
              :constraints ["fill", "[][grow][]", ""])]

    ;; set up username edit to wait for user input become idle
    (let [te-name (select form [:#username])
          lb-log (select form [:#log])
          active-chan (chan 1)
          keys-chan (chan 1)
          stop-listening (fn []
                           ;; (log lb-log "Stopping listening\n")
                           (go
                            (if (<! active-chan)
                              (do
                                ;; important to put it back to channel immediately so others could read
                                (>! active-chan false)
                                (retrieve-avatar (text te-name) lb-log)
                              ;;(log lb-log "read false from active chan")
                              ))))

          start-listening (fn []
                            ;;(log lb-log "Starting listening\n")
                            (go
                             (>! active-chan true)
                             (loop []
                               (let [[v ch] (alts! [keys-chan (timeout 800)])]
                                 (if (not (blank? v))
                                   (recur)
                                   (do
                              ;;       (log lb-log "timeout reached\n")
                                     (stop-listening))
                                   ))
                               )))]
      ;; It is important in this scheme to return value to the channel as soon as it had been read:
      ;; so that others interested can read back
      (listen te-name
              :focus-gained (fn [e]
                              (put! active-chan false))

              :document (fn [e]
                          (go
                           (if (<! active-chan)
                             (do
                               ;; important to put it back to channel immediately so others could read
                               (>! active-chan true)
                               (>! keys-chan "1")
                               )
                             (do
                               ;(log lb-log "in not-active mode, initiating listening\n")
                               (start-listening))
                             ))
                          )
              :focus-lost (fn [e]
                            ;(log lb-log "focus lost, stopping\n")
                            (stop-listening)
                            )
              )
      form)))

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

(def f (make-frame (build-content)))
(show! f)
(config! f :content (build-content))


(defn -main [& args]
  (invoke-later
   (->
    (build-content)
    (make-frame)
;;    pack!
    show!
    ))
)
