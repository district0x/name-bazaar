(ns district0x.server.macros)

(defmacro gotry [& body] 
  `(cljs.core.async.macros/go
     (try 
       ~@body 
       (catch js/Object e# 
         (taoensso.timbre/error {:error (district0x.shared.utils/jsobj->clj e#)})))))
