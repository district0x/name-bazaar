pragma solidity ^0.4.11;

import "Offering.sol";
import "./InstantBuyOfferingLibrary.sol";

contract InstantBuyOffering is Offering {

    function InstantBuyOffering(
        address _offeringRegistry,
        address _ens,
        bytes32 _node,
        string _name,
        address _originalOwner,
        address _emergencyMultisig,
        uint _price
    )
        Offering(_offeringRegistry, _ens, _node, _name, _originalOwner, _emergencyMultisig, 1, _price)
    {
    }

    function buy() payable {
        InstantBuyOfferingLibrary.buy(offering);
    }

    function setSettings(uint _price) {
        InstantBuyOfferingLibrary.setSettings(offering, _price);
    }

    function() payable {
        buy();
    }
}
