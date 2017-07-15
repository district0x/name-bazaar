pragma solidity ^0.4.11;

import "ens/AbstractENS.sol";
import "OfferingRegistry.sol";

library OfferingLibrary {

    struct Offering {
        OfferingRegistry offeringRegistry;
        AbstractENS ens;
        bytes32 node;
        string name;
        address originalOwner;
        address emergencyMultisig;
        uint offeringType;
        uint createdOn;
        address newOwner;
        uint price;
    }

    event onTransfer(address newOwner, uint price, uint datetime);

    function construct(
        Offering storage self,
        address _offeringRegistry,
        address _ens, 
        bytes32 _node, 
        string _name, 
        address _originalOwner,
        address _emergencyMultisig,
        uint _offeringType,
        uint _price
    ) {
        self.offeringRegistry = OfferingRegistry(_offeringRegistry);
        self.ens = AbstractENS(_ens);
        self.node = _node;
        self.name = _name;
        self.originalOwner = _originalOwner;
        self.emergencyMultisig = _emergencyMultisig;
        self.offeringType = _offeringType;
        self.createdOn = now;
        self.price = _price;
    }

    function reclaim(OfferingLibrary.Offering storage self) {
        var isEmergency = isSenderEmergencyMultisig(self);
        require(isEmergency || isSenderOriginalOwner(self));

        if (isContractNodeOwner(self)) {
            self.ens.setOwner(self.node, self.originalOwner);
        }
        if (isEmergency) {
            // New owner is not really this address, but it's the way to recogize it
            // was disabled in emergency without having separate var for it, which is costly
            self.newOwner = 0xdead;
        }
        setChanged(self);
    }

    // Security method in case user transfers other name to this contract than it's supposed to be
    function claim(Offering storage self, bytes32 node, address claimer) {
        require(isSenderEmergencyMultisig(self));
        require(self.node != node);
        self.ens.setOwner(node, claimer);
    }

    function finalize(Offering storage self, address _newOwner, uint _price) {
        require(!isEmergencyDisabled(self));
        require(!isFinalized(self));
        self.newOwner = _newOwner;
        self.ens.setOwner(self.node, _newOwner);
        self.originalOwner.transfer(_price);
        onTransfer(_newOwner, _price, now);
        setChanged(self);
    }

    function setChanged(Offering storage self) {
        self.offeringRegistry.setOfferingChanged(self.offeringType);
    }

    function setOfferingRegistry(Offering storage self, address _offeringRegistry) {
        require(isSenderEmergencyMultisig(self));
        self.offeringRegistry = OfferingRegistry(_offeringRegistry);
    }

    function isContractNodeOwner(Offering storage self) returns(bool) {
        return self.ens.owner(self.node) == address(this);
    }

    function isSenderOriginalOwner(Offering storage self) returns(bool) {
        return msg.sender == self.originalOwner;
    }

    function isSenderEmergencyMultisig(Offering storage self) returns(bool) {
        return msg.sender == self.emergencyMultisig;
    }

    function isEmergencyDisabled(Offering storage self) returns(bool) {
        return self.newOwner == 0xdead;
    }

    function isFinalized(Offering storage self) returns(bool) {
        return self.newOwner != 0x0;
    }
}

