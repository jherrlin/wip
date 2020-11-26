(ns client.form.hocs
  (:require
   [reagent.core :as reagent]))


(defn label [hoc-component]
  (fn [{:keys [id error-text label required? valid? visited?]
        :or {required? false}
        :as props}]
    {:pre [(string? label)]}
    [:div
     [:label {:style {:color (when (and (not valid?)
                                        visited?
                                        error-text)
                               "#a94442")}
              :for (when id id)}
      (str (when required? "* ") label)]
     [hoc-component props]]))

(defn focus-when-empty [hoc-component]
  (fn [{:keys [value id focus?]
        :or {focus? false}
        :as props}]
    {:pre [(string? id)]}
    (reagent/create-class
     {:display-name "focus-when-empty"
      :component-did-mount
      (fn []
        (when (and (empty? value)
                   focus?)
          (js/requestAnimationFrame
           #(.focus (.getElementById js/document id)))))

      :reagent-render
      (fn [props]
        [hoc-component props])})))

(defn validation-markup [hoc-component]
  (fn [{:keys [error-text valid? visited?]
        :as props}]
    [:div {:class (str "form-group has-feedback"
                       (when visited?
                         (if valid?
                           " has-success"
                           " has-error")))}
     [hoc-component props]
     (when (and (not valid?) visited?)
       [:p {:style {:color (when-not valid? "#a94442")}}
        error-text])]))
