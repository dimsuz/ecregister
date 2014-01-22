(ns ecregister.posts
  (:gen-class)
  (:require [org.httpkit.client :as http])
  (:require [net.cgrand.enlive-html :as html])
  (:use [clojure.string :only [lower-case]])
  (:use [clojure.core.async :only [chan put! >! <! <!! go go-loop]])
  )

;; How deep into AW posts history we are allowed to enter?
(def max-page-depth 32)

(defn get-html [url]
  "Fetches html and returns an unbuffered channel to which response will be put when ready"
  (let [c (chan)]
    (http/get url
              (fn [{:keys [opts status body error]}]
              (if (or error (not= status 200))
                (put! c :error) ;; error happened
                (put! c body))))
    c))

(defn link-hrefs [link-nodes]
  (map (fn [node]
         (first (html/attr-values node :href)))
       link-nodes))

(defn link-ids [link-nodes]
  (map (fn [href]
         (->> href
              (re-seq #".+?(\d+)\.html?")
              first
              second)
         )
       (link-hrefs link-nodes)))

(defn extract-fa-post [html-string]
  "Extracts a most recent post data from a given string with html content of freeaway.ru page with articles list"
  (let [tree (html/html-resource (java.io.StringReader. html-string))
        author (html/text (first (html/select tree [:a.author-link])))
        title (html/text (first (html/select tree [:.article-content :> :h1])))
        link (first (link-ids (html/select tree [:.article-links :a])))
        ;; for internal circle format is "Name (nick)", for external - just "nick"
        ic-name (->> author
                     (re-seq #".+\((\w+)\).*")
                     first
                     second)
        ]
    ;; this is how to turn some part of subtree into a html
    ;;(prn (apply str (html/emit* [(first (html/select tree [:.article-content]))])))
    {:author (if ic-name ic-name author),
     :id link,
     :title title}))

(defn extract-aw-posts [html-string]
  (let [tree (html/html-resource (java.io.StringReader. html-string))
        authors (map html/text (html/select tree [#{:.content :.topic-container} :.topic-header (html/attr= :rel "author")]))
        titles (map html/text (html/select tree [:.topic-title :> :a]))
        ids (link-ids (html/select tree [:.topic-title :> :a]))
        links (link-hrefs (html/select tree [:.topic-title :> :a]))
        ]
    (map #(zipmap [:author :title :id :link] [%1 %2 %3 %4]) authors titles ids links)
    )
  )

(defn take-until [pred coll]
  "Takes items from sequence until pred is true. Item on which pred becomes false is included as last one."
  (let [[part1 part2] (split-with pred coll)]
    (concat part1 (take 1 part2)))
      )

(defn mark-published [posts fa-post]
  "Iterates a list of posts, appending :published [true|false] key to each of them.
If 'posts' contain a published post it will be marked as such, included as a last item
and seq will be returned"
  (let [published? (fn [p]
                     (let [id1 (:id p) id2 (:id fa-post)
                           author1 (:author p) author2 (:author fa-post)]
                       (and (not (nil? id1)) (not (nil? author1)) (= id1 id2) (= (lower-case author1) (lower-case author2)))))]
;    (prn (take-until (complement published?) posts))
    (map #(conj % [:published (published? %)]) (take-until (complement published?) posts))))

(defn fetch-unpublished-aw-posts [fa-post out-chan]
  (go-loop [page 1]
           (prn "fetching posts for page " page)
           (if-let [posts (extract-aw-posts (<! (get-html (str "http://advaitaworld.com/blog/free-away/page" page))))]
             (let [marked (mark-published posts fa-post)
                   has-published? (:published (last marked))
                   unpublished (if has-published? (drop-last marked) marked)]
               ;; spit all unpub posts on this page into channel
               (doseq [post unpublished]
                      (put! out-chan post))
               (if has-published?
                 (put! out-chan :end)
                 (if (< page max-page-depth)
                   (recur (inc page))  ;; no published posts on this page return
                   (put! out-chan :error))))
             (put! out-chan :error))))

(defn fetch-latest-fa-post [out-chan]
  "Fetches a data about two most recent posts - in articles and in poetry categories, outputs to passed channel"
  (let [parsed-html-chan (chan)]
    (go
     (>! parsed-html-chan (extract-fa-post (<! (get-html "http://www.freeaway.ru/articles/all")))))
    (go
     (>! parsed-html-chan (extract-fa-post (<! (get-html "http://www.freeaway.ru/poetry/all")))))
    (go
     (let [p1 (<! parsed-html-chan)
           p2 (<! parsed-html-chan)]
       ;; note: when finding latest of two posts relying on these facts here:
       ;; a) id's are strings which are actually numbers
       ;; b) more recent posts contain bigger ids, i.e. they grow with time
       (put! out-chan (last (sort-by #(Integer/parseInt (:id %)) [p1 p2])))))
    ))

;; (let [tc (chan)]
;;   (fetch-latest-fa-post tc)
;;   (prn "Launched fetching")
;;   (let [fa-post (<!! tc)]
;;     (fetch-unpublished-aw-posts fa-post tc)
;;     (loop []
;;       (let [p (<!! tc)]
;;         (when (not= p :end)
;;           (prn p)
;;           (recur))))))
