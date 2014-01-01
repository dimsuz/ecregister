(ns ecregister.avatars
  (:gen-class)
  (:require [org.httpkit.client :as http])
  (:require [clojure.java.io :refer [copy file output-stream input-stream]])
  (:require [clojure.string :refer [blank? lower-case]])
  (:require [clojure.core.async :refer [put!]])
  (:import [javax.imageio ImageIO])
  (:import [java.awt.image BufferedImage])
  )

(defn get-avatar-url
  "Retrieves an url of user's avatar"
  ([username]
     (let [{:keys [opts body status error] :as resp} @(http/get (str "http://advaitaworld.com/profile/" username)) ]
       (if (or error (not= status 200))
         (println "[get-avatar-url] failed to retrieve url: " (opts :url) (str "[response code = " status "]" ))
         (let [matcher (re-matcher #"src=\"(.+avatar_100x100.(jpg|png))" body)
               groups (re-find matcher)
               url (second groups)
               ext (nth groups 2)
               ]
           [url ext]))))
  ([username chan]
     (http/get (str "http://advaitaworld.com/profile/" username)
               (fn [{:keys [opts status body error]}]
                 (if (or error (not= status 200))
                   (put! chan :error) ;; error happened
                   (let [matcher (re-matcher #"src=\"(.+avatar_100x100.(jpg|png))" body)
                         groups (re-find matcher)
                         url (second groups)
                         ext (nth groups 2)]
                     (put! chan [url ext])
                     ))))
     chan))

(defn read-image [url chan]
  "Retrieves and returns an image from url as a BufferedImage and puts in chan either object, or :error"
  (http/get url {:as :stream}
            (fn [{:keys [status body error]}]
              (if (or error (not= status 200))
                (put! chan :error)
                (put! chan (ImageIO/read body)))))
  chan)

(defn fetch-avatar
  "Fetches a user avatar from server and saves it to file"
  [url filepath]
  (if (blank? url)
      (println "[fetch-avatar] avatar url is empty, can not fetch")
      (let [{:keys [opts body status error] :as resp} @(http/get url { :as :stream }) ]
        (if (or error (not= status 200))
          (println "[fetch-avatar] failed to retrieve image from url: " (opts :url) (str "[response code = " status "]" ))
          (try
            (with-open [out (output-stream (file filepath))]
              (copy body out)
              (println "[fetch-avatar] saved original avatar to " filepath)
              filepath
              )
            (catch java.io.IOException e
              (println "[fetch-avatar] failed to write to file: " filepath)
              nil)
            )))))

(defn stamp-avatar
  "Stamps avatar with a provided stamp"
  [orig-path stamp-path output-path]
  (if (some blank? [orig-path stamp-path output-path])
    (println "[stamp-avatar] got an empty path, expected all of them to be not blank")
    (let [orig-img (ImageIO/read (file orig-path))
          stamp-img (ImageIO/read (file stamp-path))
          out-width (max (.getWidth orig-img) (.getWidth stamp-img))
          out-height (max (.getHeight orig-img) (.getHeight stamp-img))
          combined (new BufferedImage out-width out-height BufferedImage/TYPE_INT_RGB)
          ]
      (println "[stamp-avatar] creating stamped " out-width "x" out-height " avatar")
      (let [g (.getGraphics combined)]
        (.drawImage g orig-img 0 0 nil)
        (.drawImage g stamp-img 0 0 nil))
      (ImageIO/write combined "JPEG" (file output-path))
      (println "[stamp-avatar] wrote " output-path)
      )))

(defn stamp
  "Fetches, stamps and saves avatars (orig and stamped)"
  [name & [base]]
    (let [username (lower-case name)
          url-result (get-avatar-url username)
          url (first url-result)
          ext (second url-result)
          ava-orig-path (str "/home/dimka/free-away/avatars/orig/" username (str "." ext))
          ava-new-path (str "/home/dimka/free-away/avatars/new/" username ".jpg")
          stamp-top-path "/home/dimka/free-away/avatars/stamp_top.png"
          stamp-bot-path "/home/dimka/free-away/avatars/stamp_bot.png"
          use-top? (= "top" base)
          stamp-path (if use-top? stamp-top-path stamp-bot-path)
          ]
      (if use-top? (println "[main] using top stamp") (println "using bottom stamp"))
      (stamp-avatar (fetch-avatar url ava-orig-path) stamp-path ava-new-path)))
