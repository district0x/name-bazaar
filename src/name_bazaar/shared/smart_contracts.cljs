(ns name-bazaar.shared.smart-contracts) 

(def smart-contracts 
{:auction-offering-factory
 {:name "AuctionOfferingFactory",
  :address "0x959113e1c88d7e866e168ded02bf5cf8aaba2e0f"},
 :buy-now-offering-factory
 {:name "BuyNowOfferingFactory",
  :address "0xcaf0523f1ca2985b5cbba8a42a8cc413ff7512d0"},
 :name-bazaar-registrar
 {:name "NameBazaarRegistrar",
  :address "0x57f1887a8BF19b14fC0dF6Fd9B2acc9Af147eA85"},
 :buy-now-offering
 {:name "BuyNowOffering",
  :address "0x57001d4d427b9c862544b56b84a5520db10206b0"},
 :reverse-registrar
 {:name "ReverseRegistrar",
  :address "0x6F628b68b30Dc3c17f345c9dbBb1E483c2b7aE5c"},
 :public-resolver
 {:name "PublicResolver",
  :address "0x42D63ae25990889E35F215bC95884039Ba354115"},
 :ens
 {:name "ENSRegistry",
  :address "0x00000000000C2E074eC69A0dFb2997BA6C7d2e1e"},
 :offering-registry
 {:name "OfferingRegistry",
  :address "0x501fa189c6ed1789cf6be5728c5e4b0a94d0c4b2"},
 :district0x-emails
 {:name "District0xEmails",
  :address "0x1383405e11d179581ae3128c0c1698be29e40565"},
 :offering-requests
 {:name "OfferingRequests",
  :address "0x80afaa1b9a5717731d0d874c37e2b535dc1502a9"},
 :auction-offering
 {:name "AuctionOffering",
  :address "0x0fc5ae5770a8188e1dcf46777020c186884d44dc"}})