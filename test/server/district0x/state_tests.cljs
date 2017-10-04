(ns server.district0x.state-tests
  (:require [cljs.test :refer [deftest is testing run-tests use-fixtures]]
            [district0x.server.state :as state]))

(def atomic-test-state (atom {:some "foo"
                              :config {:private-key "foo"
                                       :public-key "bar"}}))

(def derefed-test-state {:some "FOO"
                         :config {:private-key "FU"
                                  :public-key "BAR"}})

(defn with-config [f] 
  (with-redefs [district0x.server.state/*server-state* atomic-test-state]
    (f)))

(use-fixtures :each with-config)

(deftest config-tests
  (testing "Get config key from atomic state"
    (is (= "foo" (state/config :private-key))))
  (testing "Get config key from derefed state"
    (is (= "FU" (state/config derefed-test-state :private-key))))
  (testing "Get config from atomic state"
    (is (= (-> atomic-test-state
               deref
               (get-in [:config]))
           (state/config))))
  (testing "Get config from derefed state"
    (is (= (-> derefed-test-state
               (get-in [:config]))
           (state/config derefed-test-state)))))

