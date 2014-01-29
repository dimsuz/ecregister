(ns ecregister.posts_ui
  (:gen-class)
  (:use [seesaw.core])
  (:use [seesaw.mig])
  (:use [seesaw.font])
  (:use [seesaw.border])
  (:require [ecregister.posts :as posts])
  (:require [reagi.core :as r])
  (:require [clojure.core.async :as async])
  )

(defn make-post-widget [{:keys [id author title icon visible?]}]
  (mig-panel
   :id id
   :visible? (if (nil? visible?) true visible?)
   :items [[(label :border (line-border :color "#ddd" :thickness 1) :size [42 :by 42]) "spany 2"]
           [(label :id :author :text author :font (font :name "Arial" :style :bold :size 17))
            "cell 1 0,pushy,bottom"]
           [(label :id :title :text title :font (font :name "Arial" :style :italic :size 15)) "cell 1 1,pushy,top,wmax 400px"]]
   :constraints ["", "[][]", "[][]"]
   :border (line-border :color "#ccc" :thickness 1)
   :minimum-size [400 :by 60]))

(defn setup-events [form event-stream]
  (let [fa-stream (r/filter #(= :fa-post (:id %)) event-stream)
        fa-post-stream (r/filter #(map? (:value %)) fa-stream)]
    ;; setup 'latest post' label and progressbar
    (r/map (fn [e]
             ;; TODO use cond
             (if (= :wait (:value e))
               (config! (select form [:#faprog]) :indeterminate? true)
               (let [author (:author (:value e))
                     title (:title (:value e))]
                 (config! (select form [:#latest-fa-post :> :#author]) :text author)
                 (config! (select form [:#latest-fa-post :> :#title]) :text title)
                 (config! (select form [:#faprog]) :visible? false)
                 (config! (select form [:#latest-fa-post]) :visible? true)
                 )))
           fa-stream)
    ;; when fa post is retrived, start fetching aw posts
    (r/map (fn [e]
             (let [c (async/chan)
                   fa-post (:value e)]
               ;; FIXME if c will contain :error, dismiss all fetched so far and clear listbox
               (posts/fetch-unpublished-aw-posts fa-post c)
               (async/go-loop []
                        (let [post (<! c)]
                          (when (not= :end post)
                            (add! (select form [:#aw-posts]) (make-post-widget post))
                            (recur)
                            )))
               ))
           fa-post-stream)
    ))

(defn launch-tab [event-stream]
  (let [c (async/chan)]
    (r/push! event-stream {:id :fa-post, :value :wait})
    (posts/fetch-latest-fa-post c)
    (async/go
     (r/push! event-stream {:id :fa-post, :value (async/<! c)}))))

(defn my-scrollable [& args]
  (let [s (apply scrollable args)]
    (-> s
        (.getVerticalScrollBar)
        (.setUnitIncrement 18))
    s))

(defn build-posts-tab []
  (let [form
        (mig-panel
         :items [[(label "Последний опубликованный пост") "top,center,wrap"]
                 [(progress-bar :id :faprog :indeterminate? false :value 10) "center,wrap"]
                 [(make-post-widget {:id :latest-fa-post :author "" :title "" :visible? false}) "center,wrap"]
                 [(my-scrollable (grid-panel :border 8 :id :aw-posts :vgap 10 :columns 1)) "grow,pushy"]
                 ]
         :constraints ["fill,gap 18px,hidemode 3"])
        evs (r/events)]
    (setup-events form evs)
    (launch-tab evs)
    form))
