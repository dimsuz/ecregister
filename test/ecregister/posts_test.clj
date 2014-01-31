(ns ecregister.posts
  (:use midje.sweet)
  (:require [clojure.string :as str])
  (:require [clojure.core.async :as async])
  (:require [ecregister.posts :as posts]))

(fact "'extract-fa-post' does correct parsing for internal circle"
  (let [TEST_HTML (slurp "./test/ecregister/freeaway-article-ic.html")
        parsed (extract-fa-post TEST_HTML)]
    parsed => map?
    (:author parsed) => "Floret"
    (:title parsed) => "На своих местах!!"
    (:id parsed) => "29665"
    ))

(fact "'extract-fa-post' does correct parsing for external circle"
  (let [TEST_HTML (slurp "./test/ecregister/freeaway-article-ec.html")
        parsed (extract-fa-post TEST_HTML)]
    parsed => map?
    (:author parsed) => "Ganesh"
    (:title parsed) => "Критика ФЭ"
    (:id parsed) => "29695"
    ))

(fact "'extract-aw-posts' does correct parsing"
  (let [TEST_HTML (slurp "./test/ecregister/blog-topics.html")]
    (extract-aw-posts TEST_HTML)) => (contains {:author "Shine"
                                                :title "Лекарство Приятия и Исследования"
                                                :id "30022"
                                                :link "http://advaitaworld.com/blog/free-away/30022.html"}
                                               {:author "Floret"
                                                :title "Глубина Мгновения!!"
                                                :id "30018"
                                                :link "http://advaitaworld.com/blog/free-away/30018.html"}
                                               )
  )

(facts "'mark-published'"
  (let [posts [{:author "Floret", :id "3022"},
               {:author "Ia-ha", :id "3353"}
               {:author "Shine", :id "3354"}]
        pub? (fn [p] (:published p))
        unpub? (complement pub?)
        strip-marks (fn [plist] (map #(dissoc % :published) posts))]
    (fact "returns same data marked unpublished if no published found"
      (mark-published posts {:author "NgoMa", :id "33"}) => (has every? unpub?)
      (strip-marks (mark-published posts {:author "NgoMa", :id "33"})) => posts)
    (fact "stops at last published which is found"
      (mark-published posts {:author "Ia-ha", :id "3353"}) => (just unpub? pub?))
    (fact "returns single item list if very first is published"
      (mark-published posts {:author "Floret", :id "3022"}) => (just pub?))
    (fact "ignores username case during marking"
      (mark-published posts {:author "ia-ha", :id "3353"}) => (just unpub? pub?)
      (mark-published posts {:author "ia-HA", :id "3353"}) => (just unpub? pub?))
    (fact "returns empty list if empty posts array passed"
      (mark-published [] {:author "Floret", :id "3022"}) => [])
    (fact "returns marks all unpublished if no fa-post passed"
      (mark-published posts nil) => (has every? unpub?)
      (mark-published posts {}) => (has every? unpub?)
    )
    (fact "returns all as unpublished if missing :ids or :authors"
      (mark-published [{:content "abracad"} {:content "abrac1"}]
                      {:author "Ia-ha", :id "3353"}) => (just unpub? unpub?)
      )
    )
)

(fact "'extract-full-aw-post' does correct parsing"
  (let [TEST_HTML (slurp "./test/ecregister/full-post.html")
        POST_CONTENT_HTML (clojure.string/trim (slurp "./test/ecregister/full-post-content-only.html"))]
    (extract-full-aw-post TEST_HTML {:author "NgoMa" :title "Title"}) => (just {:author "NgoMa"
                                                                                   :title "Title"
                                                                                   :content POST_CONTENT_HTML
                                                                                   :date "2014-01-27T00:18:04+04:00"
                                                                                }))

  )
