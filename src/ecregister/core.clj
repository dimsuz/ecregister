(ns ecregister.core
  (:gen-class)
  (:use [seesaw.core seesaw.mig])
  )

(defn build-avatars-tab []
  (let [bg-stamp-pos (button-group)]
       (mig-panel
        :items [[(label "Имя пользователя:") ""]
                [(text :columns 15) "wrap"]
                [(label "Положение штампа:")]
                [(flow-panel :items [(radio :id :top :text "Сверху" :group bg-stamp-pos)
                                     (radio :id :bottom :text "Снизу" :group bg-stamp-pos)]) "wrap"]
                [(button :text "Проштамповать") ""]]))
  )
;;(config! f :content (build-content))

(defn build-posts-tab []
  (label "Posts"))

(defn build-content []
  (horizontal-panel
   :items [(tabbed-panel
            :placement :bottom
            :tabs [ {:title "Аватарки" :content (build-avatars-tab)}
                    {:title "Посты" :content (build-posts-tab)}])
           ]))
;; (def f (make-frame (build-content)))
;; (show! f)

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
