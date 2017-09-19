(ns browser.key-utils-tests
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [district0x.server.key-utils :as key-utils]))

(deftest key-utils-tests
  (testing "keypair generation test."

   (prn key-utils/generate-keypair)    
   
 (is (= 1 1))

    ))
