(ns ecregister.posts
  (:use midje.sweet)
  (:require [clojure.string :as str])
  (:require [clojure.core.async :as async])
  (:require [ecregister.posts :as posts]))

(unfinished get-html)

(defn wait-res [f chan]
  (f chan)
  (first (async/alts!! [chan (async/timeout 1000)])))


(fact "'fetch-latest-fa-posts' posts an array of two elements or none if error"
  (wait-res posts/fetch-latest-fa-posts (async/chan)) => (two-of map?)
  (provided (get-html) => (async/chan))
  )

(fact "'fetch-latest-fa-posts' posts nil if error"
  (wait-res posts/fetch-latest-fa-posts (async/chan)) => nil
  (provided (get-html) => nil)
  )
;; fetch-latest-fa-posts returns nil on error
