(ns ecregister.core
  (:gen-class)
  (:use [seesaw.core])
  )

(defn build-avatars-tab []
  (label "Avatars")
  )
(defn build-posts-tab []
  (label "Posts"))

(defn build-content []
  (horizontal-panel
   :items [(tabbed-panel
            :placement :bottom
            :tabs [ {:title "Аватарки" :content (build-avatars-tab)}
                    {:title "Посты" :content (build-posts-tab)}])
           ]))
;; (config! f :content (build-content))
;; (def f (make-frame (build-content)))
;; (show! f)

(defn make-frame [content]
  (println "content " content)
  (frame
   :title "Free Away Admin"
   :on-close :hide
   :size [640 :by 480]
   :content (label "hello")
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
