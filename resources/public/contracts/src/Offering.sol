pragma solidity ^0.4.11;

import "ens/ENS.sol";

contract Offering {

    ENS public ens;
    bytes32 public node;
    string public name;
    address public originalOwner;
    address public newOwner;
    uint8 public offeringType;
    uint public createdOn;
    uint public transferredOn;

    event onCancel(uint datetime);
    event onTransfer(address newOwner, uint price, uint datetime);

    modifier onlyOriginalOwner() {
        require(msg.sender == originalOwner);
        _;
    }

    modifier contractOwnsNode() {
        require(ens.owner(node) == address(this));
        _;
    }

    function Offering(address _ens, bytes32 _node, string _name, address _originalOwner) {
        ens = ENS(_ens);
        node = _node;
        name = _name;
        originalOwner = _originalOwner;
        createdOn = now;
    }

    function cancel();
}
