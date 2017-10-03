(ns server.run-tests
  (:require [cljs.nodejs :as nodejs]
            [cljs.test :refer-macros [run-tests]]
            [server.district0x.encryption-utils-tests]
            [server.name-bazaar.smart-contract-tests]))

(nodejs/enable-util-print!)
(set! (.-error js/console) (fn [x] (.log js/console x)))

(comment
  (run-tests 'name-bazaar.smart-contract-tests))

(defn -main [& _]
  (run-tests 'server.district0x.encryption-utils-tests
             'server.name-bazaar.smart-contract-tests))

(set! *main-cli-fn* -main)

