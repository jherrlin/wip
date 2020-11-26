(ns client.form.compositions
  (:require
   [client.form.hocs :as hocs]
   [client.form.inputs :as inputs]))


(def text
  ((comp hocs/validation-markup hocs/label hocs/focus-when-empty) inputs/text))

(def textarea
  ((comp hocs/validation-markup hocs/label hocs/focus-when-empty) inputs/textarea))

(def password
  ((comp hocs/validation-markup hocs/label hocs/focus-when-empty) inputs/password))

(def number
  ((comp hocs/validation-markup hocs/label hocs/focus-when-empty) inputs/number))
