(ns ecregister.posts
  (:gen-class)
  (:require [org.httpkit.client :as http])
  (:use [clojure.core.async :as async])
  )

(defn get-html [url]
  "Fetches html and returns an unbuffered channel to which response will be put when ready"
  (let [c (chan)]
    (http/get "http://www.freeaway.ru/"
              (fn [{:keys [opts status body error]}]
              (if (or error (not= status 200))
                (put! c :error) ;; error happened
                (put! c body))))
    c))

(defn extract-post [html]
  html)

(defn fetch-latest-fa-posts [out-chan]
  "TODO"
  (let [parsed-html-chan (chan)]
    (go
     (>! parsed-html-chan (extract-post (<! (get-html "http://www.freeaway.ru/articles/all")))))
    (go
     (>! parsed-html-chan (extract-post (<! (get-html "http://www.freeaway.ru/poetry/all")))))
    (go
     (let [p1 (<! parsed-html-chan)
           p2 (<! parsed-html-chan)]
       (put! out-chan [p1 p2])))
    ))

(let [tc (chan)]
  (fetch-latest-fa-posts tc)
  (prn "Launched eval")
  (prn (<!! tc)))
