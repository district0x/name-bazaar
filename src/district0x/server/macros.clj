(ns district0x.server.macros)

(defn- compiletime-info
  [and-form ns]
  (let [meta-info (meta and-form)]
    {:ns (str (ns-name ns))
     :line (:line meta-info)
     :file (:file meta-info)}))

(defmacro gotry [& body] 
  `(cljs.core.async.macros/go
     (try 
       ~@body
       (catch js/Object e# 
         (taoensso.timbre/error "Unexpected error occured"
                                (merge {:raw-error e# :error (district0x.shared.utils/jsobj->clj e#)}
                                       ~(compiletime-info &form *ns*)))))))
