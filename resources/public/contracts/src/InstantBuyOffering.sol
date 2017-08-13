pragma solidity ^0.4.14;

import "Offering.sol";
import "InstantBuyOfferingLibrary.sol";

contract InstantBuyOffering is Offering {

    function InstantBuyOffering(
        address _offeringRegistry,
        address _registrar,
        bytes32 _node,
        string _name,
        bytes32 _labelHash,
        address _originalOwner,
        address _emergencyMultisig,
        uint _price
    )
        Offering(_offeringRegistry, _registrar, _node, _name, _labelHash, _originalOwner, _emergencyMultisig, 1, _price)
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
