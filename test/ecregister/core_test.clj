(ns ecregister.core-test
  (:require [clojure.test :refer :all]
            [ecregister.core :refer :all]
            [clojure.java.io :refer [ as-file delete-file]]))

(deftest fetch-avatar-test
  (testing "saves file"
    (let [url "http://advaitaworld.com/uploads/images/00/11/92/2012/10/27/avatar_100x100.jpg"
          output-file "/tmp/ava.jpg"]
      (is (= output-file (fetch-avatar url output-file)))
      (.exists (as-file output-file))
      (delete-file output-file)
    ))
  (testing "returns nil on failed write file"
    (let [url "http://advaitaworld.com/uploads/images/00/11/92/2012/10/27/avatar_100x100.jpg"
          output-file "/test.jpg"]
      ; no permission for writing to root
      (is (= nil (fetch-avatar url output-file)))
    ))
  (testing "returns nil if file is a directory"
    (let [url "http://advaitaworld.com/uploads/images/00/11/92/2012/10/27/avatar_100x100.jpg"
          output-file "/tmp"]
      (is (= nil (fetch-avatar url output-file)))
    ))
  )

(clojure.test/run-tests)
