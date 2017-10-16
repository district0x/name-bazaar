(ns server.district0x.state-tests
  (:require [cljs-node-io.core :as io]
            [cljs.test :refer [deftest is testing run-tests use-fixtures]]
            [district0x.server.effects :as effects]
            [district0x.server.state :as state]))

(def default-test-config {:private-key "foo"
                          :public-key "bar"
                          :nested-key {:b "same"}})

(def atomic-test-state (atom {:some "foo"
                              :config nil}))

(def derefed-test-state {:some "FOO"
                         :config {:private-key "FU"
                                  :public-key "BAR"}})

(defn with-config [f] 
  (with-redefs [district0x.server.state/*server-state* atomic-test-state]
    (goog.object/set district0x.server.effects/env "CONFIG"
                     "test/resources/test_config.json")
    (effects/load-config! atomic-test-state default-test-config)
    (f)))

(use-fixtures :each with-config)

(deftest config-tests
  (testing "Config is loaded"
    (let [loaded-config (state/config)]
      (is (= "foo" (:private-key loaded-config)))
      (is (= "bar" (:public-key loaded-config)))))

  (testing "Deep merge"
    (is (= "different" (:b (state/config :nested-key))))
    (is (= "new" (:a (state/config :nested-key)))))
  
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

