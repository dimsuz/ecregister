(ns ecregister.posts
  (:gen-class)
  (:require [org.httpkit.client :as http])
  (:require [clojure.core.async :as async])
  )

(defn fetch-latest-fa-posts [chan]
  "TODO"
  (get-html)
  (async/put! chan [{} {}]))
;.;. A journey of a thousand miles begins with a single step. --
;.;. @alanmstokes
;.;. nil
