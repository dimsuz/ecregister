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


(defn update-fa-post-widget [form e]
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

(defn fetch-aw-posts [form e]
  (let [c (async/chan)
        content-chan (async/chan)
        fa-post (:value e)]
    ;; FIXME if c will contain :error, dismiss all fetched so far and clear listbox
    (posts/fetch-unpublished-aw-posts fa-post c)
    ;; as posts arrive they get accumulated until it is known that there's no error.
    ;; when :end is received, fetching of actual post content starts
    (async/go-loop [fetched []]
      (let [post (async/<! c)]
        (cond
         (= :error post) (prn "ERROR!") ;; FIXME clear list from all fetched so far, do other cleanup stuff, disable buttons etc
         (not= :end post) (do
                            (add! (select form [:#aw-posts]) (make-post-widget post))
                            (recur (conj fetched post)))
         (= :end post) (posts/fetch-aw-posts-content fetched content-chan))))
    ;; after above function started retrieving full content, let's wait for it to arrive and accumulate posts as they go
    (async/go-loop [full-posts []]
      (let [post (async/<! content-chan)]
        (cond
         (= :error post) (prn "ERROR getting full post!")
         (not= :end post) (do
                            (prn "got full post fetched" (:title post) "by" (:author post))
                            (config! (select form [(str "#" (:id post))]) :border (line-border :color "#8e80e4" :thickness 1))
                            (recur (conj full-posts post)))
         (= :end post) (do
                         (prn "all posts fetched")
                         (config! (select form [:#publish-btn]) :enabled? (not (empty? full-posts)))
                         (config! (select form [:#publish-btn]) :user-data full-posts)
                         ))))))

(defn publish-to-fa [form e]
  (when-let [posts (config (select form [:#publish-btn]) :user-data)]
    (let [c (async/chan)]
      (when (not (empty? posts))
        (posts/send-aw-posts-to-fa-server posts c)
        (async/go
          (let [res (async/<! c)]
            (if (= :error (:id res))
              (show! (pack!
                      (dialog :content
                              (str "Failed to send: " (:value res)))))
              (show! (pack!
                      (dialog :content
                              (str "Successfully sent " (:value res) " posts")))))))))))

(defn setup-events [form event-chan map-stream]
  (listen (select form [:#publish-btn])
          :mouse-clicked (fn [e] (async/put! event-chan {:id :publish-now :value true})))

  ;; setup 'latest post' label and progressbar
  (map-stream #(= :fa-post (:id %)) (partial update-fa-post-widget form))
  ;; when fa post is retrived, start fetching aw posts
  (map-stream #(and (= :fa-post (:id %)) (map? (:value %))) (partial fetch-aw-posts form))
  (map-stream #(= :publish-now (:id %)) (partial publish-to-fa form)))

(defn launch-tab [event-chan]
  (let [c (async/chan)]
    (async/put! event-chan {:id :fa-post, :value :wait})
    (posts/fetch-latest-fa-post c)
    (async/go
     (async/put! event-chan {:id :fa-post, :value (async/<! c)}))))

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
                 [(my-scrollable (grid-panel :border 8 :id :aw-posts :vgap 10 :columns 1)) "grow,pushy,wrap"]
                 [(button :id :publish-btn :text "Опубликовать" :enabled? false) "center"]
                 ]
         :constraints ["fill,gap 18px,hidemode 3"])
        ev-chan (async/chan)
        ev-mult (async/mult ev-chan)
        tap-creator (fn [pred] (async/filter< pred (async/tap ev-mult (async/chan))))
        map-stream (fn [pred f]
                     (let [tap (tap-creator pred)]
                       (async/go-loop []
                         (let [ev (async/<! tap)]
                           (f ev)
                           (recur)))))

        ]
    (setup-events form ev-chan map-stream)
    (launch-tab ev-chan)
    form))
