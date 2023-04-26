(ns gloss.test.io
  (:use
   [clojure test])
  (:require
   [clojure.string :as str]
   [gloss.core :as gloss]
   [gloss.io :as io]
   [gloss.test.core :refer [test-roundtrip]]
   [malli.generator :as mg]
   [manifold.stream :as s]))

(deftest decode-stream
  (testing "closing the decoded stream doesn't lose data"
    (dotimes [_test-count 100]
      (let [str-frame (gloss/string "utf-8")
            in (s/stream 0 (map #(io/encode str-frame %)))
            out (io/decode-stream in str-frame)]
        (future
          (dotimes [n 10]
            @(s/put! in (str n)))
          (s/close! in))
        (is (= (map str (range 10))
               (repeatedly 10 #(deref (s/take! out))))))))

  ; for more details on the bug this tests for see:
  ; https://github.com/clj-commons/gloss/issues/62#issuecomment-1454823229
  (testing "decode-stream doesn't miss the last item of a complete source"
    (dotimes [test-count 500]
      (let [delim1 (mg/generate string? {:seed test-count :size 3})
            delim2 (mg/generate string? {:seed test-count :size 2})
            val (mg/generate string? {:seed test-count :size 5})]
        (when (not (or (str/includes? delim1 delim2)
                       (str/includes? delim2 delim1)
                       (str/includes? val delim1)
                       (str/includes? val delim2)
                       (empty? val)))
          (test-roundtrip (gloss/string :utf-8 :delimiters [delim1 (str delim1 delim2)])
                          val))))))
