(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x7a172fadb8e5a8c22c3040e3bf21e97a0068bfef"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0x380d7daea516b50985eb93c61667ae942a2b7388"},
 :registrar
 {:name "Registrar",
  :address "0xc0c7d4335fe8c91a894ce21ef607678e3af6a41e"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x6bc6a07515152ea24a7078e0175f7ba12a3e3e38"},
 :reverse-registrar
 {:name "ReverseRegistrar",
  :address "0x9062C0A6Dbd6108336BcBe4593a3D1cE05512069"},
 :deed
 {:name "Deed", :address "0x0000000000000000000000000000000000000000"},
 :public-resolver
 {:name "PublicResolver",
  :address "0x5FfC014343cd971B7eb70732021E26C35B744cc4"},
 :ens
 {:name "ENS", :address "0x051d55f06dd7789deddc4895feae981412e13752"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0xa161e1ffe0b98e123da4d68ebcf30640d3a126a4"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x8b870c68afb056c11f0344da347bcb52f693aada"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x8c60c02eba4b979801813d2ef615866884beeb22"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x6761e1245249169924d8e6b9c45d1574a71d9ef0"}})