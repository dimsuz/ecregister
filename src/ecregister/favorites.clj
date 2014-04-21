(ns ecregister.favorites
  (:gen-class)
  (:require [ecregister.posts :as posts])
  (:require [clj-time.core :as t])
  (:require [clj-time.format :as f]))

(defn parse-date [str]
  (f/parse (f/formatters :date-time-no-ms) str))

(defn is-more-recent [date post]
  "checks if post is more recent than date"
  (when-let [post-date (parse-date (:date post))]
    (t/within? (t/interval date (t/today-at 23 59))
               post-date)))

(defn print-post [p]
  (prn (format "<a href=\"%s\">%s</a>, автор <ls user=\"%s\"/> (%s голосов)"
               (:link p) (:title p) (:author p) (:fav-count p))))

(defn print-favorites [since-date]
  (let [all-posts (posts/fetch-aw-posts #(is-more-recent since-date %))]
    (dorun (take 15 (map print-post
                         (sort-by #(Integer/parseInt (:fav-count %)) #(compare %2 %1) all-posts))))
    ))

(defn print-favorites-of-month []
  ;; print favs with dates more recent than month ago
  (print-favorites (t/minus (t/today-at 0 0) (t/months 1))))

;;(print-favorites-of-month)
