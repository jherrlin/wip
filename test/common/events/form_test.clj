(ns common.events.form-test
  (:require [common.events.form :as sut]
            [clojure.test :as t]))


(def person {:name "Hannah"
             :age 33
             :country "Sweden"})

(t/deftest basic-form-usage
  (t/is (-> {}
            (sut/set-form-values          [nil :person person])
            (sut/set-form-original-values [nil :person person])
            (sut/set-form-value           [nil :person :name "Hanna"])
            (sut/get-form-changed?        [nil :person])))

  (t/is (-> {}
            (sut/set-form-visited? [nil :person :name true])
            (sut/get-form-visited? [nil :person :name])))

  (t/is (-> {}
            (sut/set-form-values          [nil :person person])
            (sut/set-form-original-values [nil :person person])
            (sut/set-form-value           [nil :person :name "Hanna"])
            (sut/set-form-visited?        [nil :person :name true])
            (sut/set-form-meta            [nil :person :submit true])
            (sut/set-form-reset           [nil :person])
            :form
            empty?))

  (t/is (= (-> {}
               (sut/set-form-values          [nil [:person "H"] person])
               (sut/set-form-original-values [nil [:person "H"] person])
               (sut/set-form-value           [nil [:person "H"] :name "Hanna"])
               (sut/set-form-visited?        [nil [:person "H"] :name true])
               (sut/set-form-meta            [nil [:person "H"] :submit true])
               (sut/get-form-values          [nil [:person "H"]]))
           {:name "Hanna", :age 33, :country "Sweden"}))
       )
