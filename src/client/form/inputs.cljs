(ns client.form.inputs
  (:require
   [clojure.spec.alpha :as s]))


(defn text [{:keys [attr id on-blur on-change on-focus placeholder value]
             :or   {placeholder ""
                    on-change   #(js/console.log "text: "  (.. % -target -value))
                    on-focus    (fn [])
                    on-blur     (fn [])}
             :as   props}]
  {:pre [(string? id)]}
  [:input.form-control
   (merge
    {:id          id
     :on-change   #(on-change (.. % -target -value))
     :on-focus    on-focus
     :on-blur     on-blur
     :type        "text"
     :placeholder placeholder
     :value       (or value "")}
    attr)])

(defn password [m]
  (text
   (assoc-in m [:attr :type] "password")))

(defn textarea [{:keys [attr id on-blur on-change on-focus placeholder value rows cols]
                 :or   {placeholder ""
                        on-change   #(js/console.log "text: "  (.. % -target -value))
                        rows        5
                        cols        33
                        on-focus    (fn [])
                        on-blur     (fn [])}
                 :as   props}]
  {:pre [(string? id)]}
  [:textarea.form-control
   (merge
    {:id          id
     :on-change   #(on-change (.. % -target -value))
     :on-focus    on-focus
     :on-blur     on-blur
     :rows        rows
     :cols        cols
     :type        "text"
     :placeholder placeholder
     :value       (or value "")}
    attr)])

(defn number [{:keys [attr id on-blur on-change on-focus placeholder value]
               :or   {placeholder ""
                      on-change   #(js/console.log "numer: "  (.. % -target -value))
                      on-focus    (fn [])
                      on-blur     (fn [])}
               :as   props}]
  {:pre [(string? id)]}
  [:input.form-control
   (merge
    {:id          id
     :on-change   #(on-change (.. % -target -value))
     :on-focus    on-focus
     :on-blur     on-blur
     :type        "number"
     :placeholder placeholder
     :value       value}
    attr)])

(defn button [{:keys [id on-click body disabled?]}]
  [:button.btn.btn-default
   {:id       "validate-save-form-button"
    :disabled disabled?
    :on-click on-click}
   body])
