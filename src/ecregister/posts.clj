(ns ecregister.posts
  (:gen-class)
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as html])
  (:use [clojure.core.async])
  )
(require '[net.cgrand.enlive-html :as html])

(defn get-html [url]
  "Fetches html and returns an unbuffered channel to which response will be put when ready"
  (let [c (chan)]
    (http/get url
              (fn [{:keys [opts status body error]}]
              (if (or error (not= status 200))
                (put! c :error) ;; error happened
                (put! c body))))
    c))

(defn extract-fa-post [html-string]
  "Extracts a most recent post data from a given string with html content of freeaway.ru page with articles list"
  (let [tree (html/html-resource (java.io.StringReader. html-string))
        author (html/text (first (html/select tree [:a.author-link])))
        title (html/text (first (html/select tree [:.article-content :> :h1])))
        content (html/text (first (html/select tree [:.article-content :> :p])))
        ]
    ;; this is how to turn some part of subtree into a html
    ;;(prn (apply str (html/emit* [(first (html/select tree [:.article-content]))])))
    {:author (->> author
              (re-seq #".+\((\w+)\).*")
              first
              second),
     :title title, :content content}))

(defn extract-aw-posts [html-string]
  (let [tree (html/html-resource (java.io.StringReader. html-string))
        topics (html/select tree [#{:content :.topic-container}])]
    (count topics)
    )
  )

(defn fetch-latest-fa-posts [out-chan]
  "Fetches a data about two most recent posts - in articles and in poetry categories, outputs to passed channel"
  (let [parsed-html-chan (chan)]
    (go
     (>! parsed-html-chan (extract-fa-post (<! (get-html "http://www.freeaway.ru/articles/all")))))
    (go
     (>! parsed-html-chan (extract-fa-post (<! (get-html "http://www.freeaway.ru/poetry/all")))))
    (go
     (let [p1 (<! parsed-html-chan)
           p2 (<! parsed-html-chan)]
       (put! out-chan [p1 p2])))
    ))

;; (let [tc (chan)]
;;   (fetch-latest-fa-posts tc)
;;   (prn "Launched eval")
;;   (prn (<!! tc)))
