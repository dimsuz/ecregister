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

;; to be evaluated in *scratch*, to be executed in clj buffer
;; (define-key clojure-mode-map (kbd "C-c SPC")
;;   (lambda ()
;;     (interactive)
;;     (cider-interactive-eval
;;      ;; customize this to liking per dev session needs...
;;      "(config! f :content (build-content))")))

(def state (atom {:stamp-type :bottom}))
(defn update-state [& args]
  (apply swap! state assoc args))

;;(require '[clojure.core.async :refer [chan >!! <!! <! >! alts!! alts! timeout thread put! go go-loop close!]])
(defn prepare-avatars [username lb-log form]
  "Fetches an avatar from server, fills in image views, stamps it"
  (when (not (blank? username))
    (log lb-log (str "retrieving avatar for " username "\n"))
    (go
     (let [c (chan)
           resp (<! (av/get-avatar-url username c))]
       (if (= resp :error)
         (log lb-log (str "failed to retrieve avatar image for '" username "'\n"))
         (let [image (<! (av/read-image (first resp) c))
               ext (second resp)]
           (log lb-log (str "successfully read image, detected extension is '" ext "'\n"))
           (config! (select form [:#orig-ava]) :icon image)
           (update-state :image-orig image :image-ext ext)
           (update-stamped-image form)
           ))
       ))))

(defn update-stamped-image [form]
  "Gets an image from state, [re]stamps it according to state config,
saves newly stamped to state updates widgets"
  (when-let [image-orig (:image-orig @state)]
    (update-state :image-stamped
                  (av/stamped image-orig (:stamp-type @state) (:image-ext @state)))
    (config! (select form [:#stamped-ava]) :icon (:image-stamped @state))))

(defn build-avatars-tab []
  (let [bg-stamp-pos (button-group)
        form (mig-panel
              :items [[(label "Имя пользователя:") ""]
                      [(text :id :username :columns 15)]
                      [(label :id :orig-ava
                              :border (line-border :color "#ddd" :thickness 1)
                              :halign :center)
                       "span 1 3,w 105px::, h 105px::,wrap,top"]
                      [(label "Положение штампа:")]
                      [(flow-panel :items [(radio :id :top
                                                  :text "Сверху"
                                                  :group bg-stamp-pos
                                                  :selected? (not= :bottom (:stamp-type @state)))
                                           (radio :id :bottom
                                                  :text "Снизу"
                                                  :group bg-stamp-pos
                                                  :selected? (= :bottom (:stamp-type @state))
                                                  )]) "wrap"]
                      [(button :text "Проштамповать") "wrap,skip 1"]
                      [(label "") "grow,push,span 2"] ;; empty filler
                      [(label :id :stamped-ava
                              :border (line-border :color "#ddd" :thickness 1)
                              :halign :center)
                       "w 105px::, h 105px::,wrap,top,gaptop 20px"]
                      [(scrollable (log-window :id :log
                                               :rows 8
                                               :columns 80
                                               ))
                       "span,growx"]]
              :constraints ["fill", "[][grow][]", ""])
        te-name (select form [:#username])
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
                              (prepare-avatars (text te-name) lb-log form)
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
                             (>! keys-chan "1"))
                           (do
                                        ;(log lb-log "in not-active mode, initiating listening\n")
                             (start-listening)))))
            :focus-lost (fn [e]
                                        ;(log lb-log "focus lost, stopping\n")
                          (stop-listening)))
    (listen bg-stamp-pos :selection
            (fn [e]
              (when-let [image (:image-orig @state)]
                (when-let [s (selection bg-stamp-pos)]
                  (update-state :stamp-type (config s :id))
                  (update-stamped-image form)
                  ))))
    form))

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
