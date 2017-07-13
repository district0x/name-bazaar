pragma solidity ^0.4.11;

import "OfferingLibrary.sol";

library InstantBuyOfferingLibrary {
    using OfferingLibrary for OfferingLibrary.Offering;

    struct InstantBuyOffering {
        uint price;
    }

    function buy(
        InstantBuyOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        require(msg.value == self.price);
        offering.finalize(msg.sender, msg.value);
        offering.setChanged();
    }

    function setSettings(
        InstantBuyOffering storage self,
        OfferingLibrary.Offering storage offering,
        uint _price
    ) {
        require(offering.isSenderOriginalOwner());
        require(!offering.isFinalized());
        self.price = _price;
        offering.setChanged();
    }
}
