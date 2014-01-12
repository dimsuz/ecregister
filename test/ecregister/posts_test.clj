(ns ecregister.posts
  (:use midje.sweet)
  (:require [clojure.string :as str])
  (:require [clojure.core.async :as async])
  (:require [ecregister.posts :as posts]))

(fact "'extract-fa-post' does correct parsing"
  (let [TEST_HTML "<html><body><div class=\"paginatable-section\"><div class=\"article-header\"> <div class=\"avatar pull-left\"> <span><img src=\"/bundles/freeawaymain/img/avatars/floret.png?v33\"></span> </div> <div class=\"author pull-left\"> <div class=\"dropdown\"> <a href=\"#\" role=\"button\" class=\"dropdown-toggle author-link\" data-toggle=\"dropdown\" data-hover=\"dropdown\" data-delay=\"200\">Ольга (Floret)</a> <ul class=\"dropdown-menu\" role=\"menu\"> <li><a tabindex=\"-1\" href=\"/bio/Floret\">Биография</a></li> <li><a tabindex=\"-1\" href=\"/articles/Floret\">Публикации</a></li> <li><a tabindex=\"-1\" href=\"/creation/Floret\">Творчество</a></li> <li><a tabindex=\"-1\" href=\"http://advaitaworld.com/journal/Floret\">Личный блог</a></li> <li><span>Встреч нет</span></li> </ul> </div> </div> <div class=\"date pull-right\"> <span>02 января 2014</span> </div> </div> <div id=\"post1156\" class=\"article-content with-link \"> <h1>На своих местах!!</h1> <p>Всё всегда на своих местах, с которых никуда ничего и не убегало!!!<br>Всё так, а не иначе, потому что УЖЕ на месте!!!<br>Каждое, что происходит, каждое что случается, что помыслено!!!<br>Любое явление имеет свою окраску, свой рисунок, название!&nbsp;<br>И уже ТУТ!!!<br>Ничего не имеет ошибки, но ничего и не идеально!!!<br>Всё Абсолютно в относительном!!!<br>И ничто не является Абсолютом!!!&nbsp;<br>Вот и Вся КРАСОТА!!!</p> </div> <div class=\"article-links\"> <p class=\"pull-right\"><a href=\"http://advaitaworld.com/blog/free-away/29665.html \" target=\"_blank\">Комментировать</a></p><p> </p></div> <div class=\"article-separator\"></div> </div></body></html>"
        parsed (extract-fa-post TEST_HTML)]
    parsed => map?
    (:author parsed) => "Floret"
    (:title parsed) => "На своих местах!!"
    (:id parsed) => "29665"
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
