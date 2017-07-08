pragma solidity ^0.4.11;

import "ens/AbstractENS.sol";

library OfferingLibrary {

    struct Offering {
        AbstractENS ens;
        bytes32 node;
        string name;
        address originalOwner;
        address emergencyMultisig;
        uint offeringType;
        uint contractVersion;
        address newOwner;
    }

    event onReclaim(uint datetime, bool isEmergency);
    event onTransfer(address newOwner, uint price, uint datetime);

    function construct(
        Offering storage self,
        address _ens, 
        bytes32 _node, 
        string _name, 
        address _originalOwner,
        address _emergencyMultisig,
        uint _offeringType,
        uint _contractVersion
    ) {
        self.ens = AbstractENS(_ens);
        self.node = _node;
        self.name = _name;
        self.originalOwner = _originalOwner;
        self.emergencyMultisig = _emergencyMultisig;
        self.offeringType = _offeringType;
        self.contractVersion = _contractVersion;
    }

    function reclaim(OfferingLibrary.Offering storage self) {
        var isEmergency = isSenderEmergencyMultisig(self);
        require(isEmergency || isSenderOriginalOwner(self));

        if (isContractNodeOwner(self)) {
            self.ens.setOwner(self.node, self.originalOwner);
            onReclaim(now, isEmergency);
        }
        if (isEmergency) {
            // New owner is not really this address, but it's the way to recogize it
            // was disabled in emergency without having separate var for it, which is costly
            self.newOwner = 0xdead;
        }
    }

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

