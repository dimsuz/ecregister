(ns ecregister.avatars
  (:gen-class)
  (:require [org.httpkit.client :as http])
  (:require [clojure.java.io :refer [copy file output-stream input-stream]])
  (:require [clojure.string :refer [blank? lower-case]])
  (:require [clojure.core.async :refer [put!]])
  (:import [javax.imageio ImageIO ImageWriteParam IIOImage])
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
                     (if (> (count groups) 2)
                       (put! chan [url ext])
                       (put! chan :error))
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

(defn has-alpha? [image]
  (and (not (nil? image)) (or
                           (= BufferedImage/TYPE_INT_ARGB (.getType image))
                           (= BufferedImage/TYPE_4BYTE_ABGR (.getType image))
                           (= BufferedImage/TYPE_INT_ARGB_PRE (.getType image))
                           (= BufferedImage/TYPE_4BYTE_ABGR_PRE (.getType image))
                           )))

(defn write-image [output-dir image-name image]
  "Writes an image to a file, returns a saved image file name"
  (when (every? #(not (nil? %)) [output-dir image-name image])
    (let [has-alpha? (has-alpha? image)
          ext (if has-alpha? "png" "jpg")
          filename (str image-name "." ext)
          out-file (file (str output-dir filename))]
      (if has-alpha?
        (ImageIO/write image "png" out-file)
        ; else need to tweak jpeg compression quality
        (let [writer (.next (ImageIO/getImageWritersByFormatName "jpeg"))
              param (.getDefaultWriteParam writer)
              ios (ImageIO/createImageOutputStream out-file)]
          (.setCompressionMode param ImageWriteParam/MODE_EXPLICIT)
          (.setCompressionQuality param 1.0)
          (.setOutput writer ios)
          (.write writer nil (IIOImage. image nil nil) param)
          (.dispose writer)
          ))
      filename)))

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

(defn stamped [image stamp-path image-ext]
  "Stamps passed image with stamp of passed type"
  (let [stamp-img (ImageIO/read (file stamp-path))
        out-width (max (.getWidth image) (.getWidth stamp-img))
        out-height (max (.getHeight image) (.getHeight stamp-img))
        need-alpha? (if (re-find #"(png|gif)$" image-ext) true false)
        combined (BufferedImage. out-width out-height
                                 (if need-alpha? BufferedImage/TYPE_INT_ARGB BufferedImage/TYPE_INT_RGB))
        g (.getGraphics combined)]
    (.drawImage g image 0 0 nil)
    (.drawImage g stamp-img 0 0 nil)
    combined))

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
