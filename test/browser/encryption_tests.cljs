(ns browser.encryption-tests
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [district.encryption :as encryption]))

(deftest encryption-tests
    
  (testing "content encryption test."
    (let [keypair (encryption/generate-keypair)
          content "top-secret@district0x.io"
          base64-encrypted-content (->> content
                                        (encryption/encrypt (:public-key keypair))
                                        (encryption/encode-base64))
          decoded-content (->> base64-encrypted-content
                               (encryption/decode-base64)
                               (encryption/decrypt (:private-key keypair)))]
      (is (= content decoded-content)))))
