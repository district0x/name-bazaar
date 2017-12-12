(ns server.run-tests
  (:require
    [cljs.nodejs :as nodejs]
    [cljs.pprint]
    [cljs.test :refer [run-tests]]
    [server.name-bazaar.smart-contract-altering-tests]
    [server.name-bazaar.smart-contract-ext-tests]
    [server.name-bazaar.smart-contract-tests]))

(nodejs/enable-util-print!)

(set! (.-error js/console) (fn [x] (.log js/console x)))

(comment
  (run-tests 'server.name-bazaar.smart-contract-tests
             'server.name-bazaar.smart-contract-ext-tests
             'server.name-bazaar.smart-contract-altering-tests))

(defn -main [& _]
  (run-tests 'server.name-bazaar.smart-contract-tests
             'server.name-bazaar.smart-contract-ext-tests
             'server.name-bazaar.smart-contract-altering-tests))

(set! *main-cli-fn* -main)