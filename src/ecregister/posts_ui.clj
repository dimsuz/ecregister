(ns ecregister.posts_ui
  (:gen-class)
  (:use [seesaw.core])
  (:use [seesaw.mig])
  (:use [seesaw.font])
  (:use [seesaw.border])
  )
(defn make-post-widget [{:keys [author title icon]}]
  (mig-panel
   :items [[(label :border (line-border :color "#ddd" :thickness 1) :size [42 :by 42]) "spany 2"]
           [(label :text author :font (font :name "Arial" :style :bold :size 17))
            "cell 1 0,pushy,bottom"]
           [(label :text title :font (font :name "Arial" :style :italic :size 15)) "cell 1 1,pushy,top"]]
   :constraints ["", "[][]", "[][]"]
   :border (line-border :color "#ccc" :thickness 1)
   :minimum-size [400 :by 0]))

(defn build-posts-tab []
  (let [form
        (mig-panel
         :items [[(label "Последний опубликованный пост") "top,center,wrap"]
                 [(progress-bar :indeterminate? false :value 10) "center,wrap"]
                 [(make-post-widget {:author "Dima" :title "Как я что-то сделал"}) "center,wrap"]
                 [(make-post-widget {:author "Anton" :title "Как я что-то сделал"}) "center,wrap"]
                 ]
         :constraints ["fillx,gap 18px"]
         )]
    form))
