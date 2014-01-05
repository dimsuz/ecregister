(ns ecregister.avatars
  (:gen-class)
  (:require [org.httpkit.client :as http])
  (:require [clojure.java.io :refer [copy file output-stream]])
  (:require [clojure.string :refer [blank? lower-case]])
  (:require [clojure.core.async :refer [put!]])
  (:import [javax.imageio ImageIO ImageWriteParam IIOImage])
  (:import [java.awt.image BufferedImage])
  )

(defn get-avatar-url [username chan]
  "Retrieves an url of user's avatar"
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
  chan)

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
