(ns client.debug)


(defn map-pre [{:keys [m id]}]
  [:div {:style {:width "100%"}}
   [:pre (when id {:id id})
    (with-out-str (cljs.pprint/pprint (or m @re-frame.db/app-db)))]])
