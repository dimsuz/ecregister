(ns ecregister.core
  (:gen-class)
  (:use [seesaw.core])
  (:use [seesaw.mig])
  (:use [seesaw.border])
  (:use [seesaw.swingx])
  (:use [seesaw.widgets.log-window])
  (:require [clojure.string :refer [blank?]])
  (:require [clojure.core.async :refer [chan >!! <!! <! >! alts!! alts! timeout thread put! go go-loop close!]])
  (:require [ecregister.avatars :as av])
  )

;; to be evaluated in *scratch*, to be executed in clj buffer
;; (define-key clojure-mode-map (kbd "C-c SPC")
;;   (lambda ()
;;     (interactive)
;;     (cider-interactive-eval
;;      ;; customize this to liking per dev session needs...
;;      "(config! f :content (build-content))")))

(def state (atom {}))
(defn update-state [& args]
  (apply swap! state assoc args))
(defn reset-state []
  (reset! state {:stamp-type :bottom
                :stamp-path {:bottom "/home/dimka/free-away/avatars/stamp_bot.png"
                             :top    "/home/dimka/free-away/avatars/stamp_top.png"}
                :save-dir-orig "/home/dimka/free-away/avatars/orig/"
                :save-dir-new "/home/dimka/free-away/avatars/new/"
                }))

(defn update-stamped-image [form]
  "Gets an image from state, [re]stamps it according to state config,
saves newly stamped to state updates widgets"
  (when-let [image-orig (:image-orig @state)]
    (update-state :image-stamped
                  (av/stamped image-orig
                              (get-in @state [:stamp-path (:stamp-type @state)])
                              (:image-ext @state)))
    (config! (select form [:#stamped-ava]) :icon (:image-stamped @state))
    (let [filename (str (:username @state) "." (:image-ext @state))]
      (config! (select form [:#save-label-orig])
               :text (str (:save-dir-orig @state) filename))
      (config! (select form [:#save-label-new])
               :text (str (:save-dir-new @state) filename)))))

(defn show-busy-indicators [show? form]
  (let [label-ava-orig (select form [:#orig-ava])
        label-ava-stamped (select form [:#stamped-ava])]
    (if show?
      (do
        (config! label-ava-orig :icon (config label-ava-orig :user-data))
        (config! label-ava-orig :busy? true)
        (config! label-ava-stamped :icon (config label-ava-stamped :user-data))
        (config! label-ava-stamped :busy? true))
      (do
        ;; save their custom icons and temporarily replace them with nil ones
        ;; this is done to hide busy indicator on start and let it by shown only later before busy anim is launched
        (config! label-ava-orig :user-data (config label-ava-orig :icon))
        (config! label-ava-orig :icon nil)
        (config! label-ava-stamped :user-data (config label-ava-stamped :icon))
        (config! label-ava-stamped :icon nil)
        ))))

(defn prepare-avatars [username lb-log form]
  "Fetches an avatar from server, fills in image views, stamps it"
  (when (not (blank? username))
    (log lb-log (str "retrieving avatar for " username "\n"))
    (go
     (let [c (chan)
           resp (<! (av/get-avatar-url username c))]
       (show-busy-indicators false form)
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

(defn build-avatars-tab []
  (reset-state)
  (let [bg-stamp-pos (button-group)
        form (mig-panel
              :items [[(label "Имя пользователя:") ""]
                      [(text :id :username :columns 15)]
                      [(vertical-panel
                        :items [(busy-label :id :orig-ava
                                       :border (line-border :color "#ddd" :thickness 1)
                                       :halign :center
                                       :valign :center
                                       :size [106 :by 106]) [:fill-v 10]
                                (busy-label :id :stamped-ava
                                       :border (line-border :color "#ddd" :thickness 1)
                                       :halign :center
                                       :valign :center
                                       :size [106 :by 106]
                                       )])
                       "span 1 7,wrap,top"]
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
                      [(label
                        :foreground "#bbb"
                        :font "Terminus"
                        :text "Аватарки будут сохранены в:") "span 2,wrap"]
                      [(label :id :save-label-orig
                              :font "Terminus"
                              :foreground "#bbb"
                              :text (:save-dir-orig @state)) "span 2,wrap"]
                      [(label :id :save-label-new
                              :font "Terminus"
                              :foreground "#bbb"
                              :text (:save-dir-new @state)) "span 2,wrap"]
                      [(button :id :button-stamp :text "Проштамповать") "wrap,skip 1"]
                      [(label "") "grow,push,span 2,wrap"] ;; empty filler
                      [(scrollable (log-window :id :log
                                               :rows 8
                                               :columns 80
                                               ))
                       "span,growx"]]
              :constraints ["fill", "[][grow][]", ""])
        te-name (select form [:#username])
        lb-log (select form [:#log])
        button-stamp (select form [:#button-stamp])
        have-images? #(and (contains? @state :image-orig) (contains? @state :image-stamped))
        active-chan (chan 1)
        keys-chan (chan 1)
        stop-listening (fn []
                         ;; (log lb-log "Stopping listening\n")
                         (go
                          (if (<! active-chan)
                            (do
                              ;; important to put it back to channel immediately so others could read
                              (>! active-chan false)
                              (update-state :username (text te-name))
                              (show-busy-indicators true form)
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
    (listen button-stamp :action
            (fn [e]
              (when (have-images?)
                (let [args1 [(:save-dir-orig @state) (:username @state) (:image-orig @state)]
                      args2 [(:save-dir-new @state) (:username @state) (:image-stamped @state)]
                      file1 (apply av/write-image args1)
                      file2 (apply av/write-image args2)]
                  (log lb-log (str "Saved " (:save-dir-orig @state) file1 "\n"))
                  (log lb-log (str "Saved " (:save-dir-new @state) file2 "\n"))
                  )
                )
              ))
    (show-busy-indicators false form)
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
   :content (build-content)
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
