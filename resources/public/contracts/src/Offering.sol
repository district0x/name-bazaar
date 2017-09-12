pragma solidity ^0.4.14;

import "OfferingLibrary.sol";

contract Offering {
    using OfferingLibrary for OfferingLibrary.Offering;

    OfferingLibrary.Offering public offering;

    event onTransfer(address newOwner, uint price, uint datetime);

    function Offering(
        address _offeringRegistry,
        address _registrar,
        bytes32 _node,
        string _name,
        bytes32 _labelHash,
        address _originalOwner,
        address _emergencyMultisig,
        uint _version,
        uint _price
    ) {
        offering.construct(
            _offeringRegistry,
            _registrar,
            _node,
            _name,
            _labelHash,
            _originalOwner,
            _emergencyMultisig,
            _version,
            _price
        );
    }

    function reclaimOwnership() {
        offering.reclaimOwnership();
    }

    function setOfferingRegistry(address _offeringRegistry) {
        offering.setOfferingRegistry(_offeringRegistry);
    }
}
