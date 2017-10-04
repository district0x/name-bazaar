pragma solidity ^0.4.14;

/**
 * @title AuctionOfferingLibrary
 * @dev Contains logic for AuctionOffering methods
 */

import "SafeMath.sol";
import "OfferingLibrary.sol";

library AuctionOfferingLibrary {
    using SafeMath for uint;
    using OfferingLibrary for OfferingLibrary.Offering;

    struct AuctionOffering {
        uint  endTime;                              // end time of the auction
        uint  extensionDuration;                    // If new highest bid arrives less than extensionDuration before
                                                    // auction end time, the auction will be extended by another extensionDuration
        uint  minBidIncrease;                       // Min. amount new bid must be higher than previous one
        address winningBidder;                      // Address of currently winning bidder
        uint bidCount;                              // Number of bids for this auction
        mapping(address => uint) pendingReturns;    // Map of pending returns for overbid bidders
    }

    function construct(
        AuctionOffering storage self,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        require(_endTime > now);
        self.endTime = _endTime;
        self.extensionDuration = _extensionDuration;
        require(_minBidIncrease > 0);
        self.minBidIncrease = _minBidIncrease;
    }

    /**
    * @dev Makes a new bid in an auction
    * Puts funds of previous winning bidder available for withdrawal
    * Extends auction if bid happens within extensionDuration
    * Bid must be higher than minBidIncrease if other than first bid
    * Cannot be after end time of auction
    * Offering contract must own ENS name
    * @param self AuctionOffering AuctionOffering data struct
    * @param offering OfferingLibrary.Offering Offering data struct
    */
    function bid(
        AuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        require(now < self.endTime);
        require(offering.isContractNodeOwner());

        uint bidValue;
        if (msg.sender == self.winningBidder) {
          //Overbidding oneself
          bidValue = offering.price.add(msg.value);
        } else {
          //Overbidding someone else
          bidValue = self.pendingReturns[msg.sender].add(msg.value);
          self.pendingReturns[msg.sender] = 0;
        }

        if (self.winningBidder == 0x0) {
            require(bidValue >= offering.price);
        } else {
            require(bidValue >= offering.price.add(self.minBidIncrease));
            if (msg.sender != self.winningBidder)
              self.pendingReturns[self.winningBidder] = self.pendingReturns[self.winningBidder].add(offering.price);
        }

        self.winningBidder = msg.sender;
        self.bidCount += 1;
        offering.price = bidValue;

        if ((self.endTime - self.extensionDuration) <= now) {
            self.endTime = now.add(self.extensionDuration);
        }

        var extraEventData = new uint[](3);
        extraEventData[0] = uint(msg.sender);
        extraEventData[1] = offering.price;
        extraEventData[2] = now;
        offering.fireOnChanged("bid", extraEventData);
    }

    /**
    * @dev Withdraws pending returns of a bidder
    * Only original bidder or emergency multisig can withdraw funds,
    * In each case funds are sent to original bidder only
    * @param self AuctionOffering AuctionOffering data struct
    * @param offering OfferingLibrary.Offering Offering data struct
    * @param _address address The address of withdrawer
    */
    function withdraw(
        AuctionOffering storage self,
        OfferingLibrary.Offering storage offering,
        address _address
    ) {
        require(msg.sender == _address || offering.isSenderEmergencyMultisig());
        var pendingReturns = self.pendingReturns[_address];
        if (pendingReturns > 0) {
            self.pendingReturns[_address] = 0;
            _address.transfer(pendingReturns);
            offering.fireOnChanged("withdraw");
        }
    }

    /**
    * @dev Finalizes auction: transfers funds to original ENS owner, transfers ENS name to winning bidder
    * Must be after auction end time
    * Accoring to Withdrawal Pattern we cannot assume transferring funds to original owner will be possible,
    * therefore should put his funds for withdrawal, rather than transferring. However, to not do this UX nightmare
    * for our users, we offer both ways determined by `transferPrice`, where in UI withdrawal pattern is used only as fallback
    * @param self AuctionOffering AuctionOffering data struct
    * @param offering OfferingLibrary.Offering Offering data struct
    * @param transferPrice bool Whether to to actual transfer of funds or just put them available for withdrawal
    */
    function finalize(
        AuctionOffering storage self,
        OfferingLibrary.Offering storage offering,
        bool transferPrice
    ) {
        require(now > self.endTime);
        require(self.winningBidder != 0x0);
        offering.transferOwnership(self.winningBidder);

        if (transferPrice) {
            offering.originalOwner.transfer(offering.price);
        } else {
            self.pendingReturns[offering.originalOwner] =
                self.pendingReturns[offering.originalOwner].add(offering.price);
        }
    }

    /**
    * @dev Reclaims ownership of ENS name back to original owner
    * Can be done only if auction has no bids
    * Emergency multisig can do this even for auction with bids and it puts winner bidder funds for withdrawal
    * @param self AuctionOffering AuctionOffering data struct
    * @param offering OfferingLibrary.Offering Offering data struct
    */
    function reclaimOwnership(
        AuctionOffering storage self,
        OfferingLibrary.Offering storage offering
    ) {
        if (offering.isSenderEmergencyMultisig()) {
            if (!hasNoBids(self) && !offering.wasEmergencyCancelled()) {
                self.pendingReturns[self.winningBidder] = offering.price;
            }
        } else {
            require(hasNoBids(self));
        }
        offering.reclaimOwnership();
    }

    /**
    * @dev Changes settings for AuctionOffering
    * Can be executed only by original owner
    * Can't be executed when auction already has some bids
    * @param offering OfferingLibrary.Offering Offering data struct
    * @param _startPrice uint New start price of the auction
    * @param _endTime uint New end time of the auction
    * @param _extensionDuration uint New extension duration of the auction
    * @param _minBidIncrease uint New min bid increase of the auction
    */
    function setSettings(
        AuctionOffering storage self,
        OfferingLibrary.Offering storage offering,
        uint _startPrice,
        uint _endTime,
        uint _extensionDuration,
        uint _minBidIncrease
    ) {
        require(offering.isSenderOriginalOwner());
        require(hasNoBids(self));
        offering.price = _startPrice;

        construct(
            self,
            _endTime,
            _extensionDuration,
            _minBidIncrease
        );
        offering.fireOnChanged("setSettings");
    }

    /**
    * @dev Returns whether auction already has no bids
    * @param self AuctionOffering AuctionOffering data struct
    * @return bool true if auction has no bids
    */
    function hasNoBids(AuctionOffering storage self) returns(bool) {
        return self.winningBidder == 0x0;
    }

    /**
    * @dev Returns amount of pending returns ready for withdrawal for a given address
    * @param self AuctionOffering AuctionOffering data struct
    * @param bidder address The address with pending returns
    * @return uint The amount of pending returns
    */
    function pendingReturns(AuctionOffering storage self, address bidder) returns (uint) {
        return self.pendingReturns[bidder];
    }
}

