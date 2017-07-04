pragma solidity ^0.4.11;

import "Offering.sol";

contract InstantBuyOffering is Offering {

    uint8 public offeringType = 1;
    uint16 public contractVersion = 1;

    uint public price;

    event onSettingsChanged(uint price);

    function InstantBuyOffering(
        address _ens,
        bytes32 _node,
        string _name,
        address _originalOwner,
        uint _price
    )
        Offering(_ens, _node, _name, _originalOwner)
    {
        price = _price;
    }

    function buy()
        payable
        contractOwnsNode
    {
        require(msg.value == price);
        ens.setOwner(node, msg.sender);
        originalOwner.transfer(msg.value);
        newOwner = msg.sender;
        transferredOn = now;
        onTransfer(msg.sender, price, now);
    }

    function cancel()
        onlyOriginalOwner
        contractOwnsNode
    {
        ens.setOwner(node, originalOwner);
        onCancel(now);
    }

    function setSettings(uint _price)
        onlyOriginalOwner
        contractOwnsNode
    {
        price = _price;
        onSettingsChanged(_price);
    }

    function()
        payable
    {
        buy();
    }
}
