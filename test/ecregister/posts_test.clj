(ns ecregister.posts
  (:use midje.sweet)
  (:require [clojure.string :as str])
  (:require [clojure.core.async :as async])
  (:require [ecregister.posts :as posts]))

(unfinished get-html)
(use 'midje.sweet :reload)

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



;.;. Whoever wants to reach a distant goal must take small steps. --
;.;. fortune cookie
(fact "'extract-fa-post' does correct parsing"
  (let [TEST_HTML "<div class=\"paginatable-section\"><div class=\"article-header\"> <div class=\"avatar pull-left\"> <span><img src=\"/bundles/freeawaymain/img/avatars/floret.png?v33\"></span> </div> <div class=\"author pull-left\"> <div class=\"dropdown\"> <a href=\"#\" role=\"button\" class=\"dropdown-toggle author-link\" data-toggle=\"dropdown\" data-hover=\"dropdown\" data-delay=\"200\">Ольга (Floret)</a> <ul class=\"dropdown-menu\" role=\"menu\"> <li><a tabindex=\"-1\" href=\"/bio/Floret\">Биография</a></li> <li><a tabindex=\"-1\" href=\"/articles/Floret\">Публикации</a></li> <li><a tabindex=\"-1\" href=\"/creation/Floret\">Творчество</a></li> <li><a tabindex=\"-1\" href=\"http://advaitaworld.com/journal/Floret\">Личный блог</a></li> <li><span>Встреч нет</span></li> </ul> </div> </div> <div class=\"date pull-right\"> <span>02 января 2014</span> </div> </div> <div id=\"post1156\" class=\"article-content with-link \"> <h1>На своих местах!!</h1> <p>Всё всегда на своих местах, с которых никуда ничего и не убегало!!!<br>Всё так, а не иначе, потому что УЖЕ на месте!!!<br>Каждое, что происходит, каждое что случается, что помыслено!!!<br>Любое явление имеет свою окраску, свой рисунок, название!&nbsp;<br>И уже ТУТ!!!<br>Ничего не имеет ошибки, но ничего и не идеально!!!<br>Всё Абсолютно в относительном!!!<br>И ничто не является Абсолютом!!!&nbsp;<br>Вот и Вся КРАСОТА!!!</p> </div> <div class=\"article-links\"> <p class=\"pull-right\"><a href=\"http://advaitaworld.com/blog/free-away/29665.html \" target=\"_blank\">Комментировать</a></p><p> </p></div> <div class=\"article-separator\"></div> </div>"
        parsed (extract-fa-post TEST_HTML)]
    parsed => map?
    (:author parsed) => "Floret"
    (:title parsed) => "На своих местах!!"
    (:content parsed) => (has-prefix "Всё всегда на своих")
    ))
