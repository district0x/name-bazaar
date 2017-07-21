pragma solidity ^0.4.11;

import "OfferingLibrary.sol";

library InstantBuyOfferingLibrary {
    using OfferingLibrary for OfferingLibrary.Offering;

    function buy(
        OfferingLibrary.Offering storage offering
    ) {
        require(msg.value == offering.price);
        offering.originalOwner.transfer(offering.price);
        offering.transferOwnership(msg.sender, msg.value);
    }

    function setSettings(
        OfferingLibrary.Offering storage offering,
        uint _price
    ) {
        require(offering.isSenderOriginalOwner());
        require(!offering.wasOwnershipTransferred());
        offering.price = _price;
        offering.setChanged();
    }
}
