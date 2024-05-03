package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.BalanceHistoryRepository;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.Map;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
     @Autowired
     private CreditCardRepository creditCardRepository;

     @Autowired
     private UserRepository userRepository;
   
     @Autowired
     private BalanceHistoryRepository balanceHistoryRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length

        // Create an instance of new creditcard with payload information
        CreditCard creditCard = new CreditCard();

        creditCard.setIssuanceBank(payload.getCardIssuanceBank());
        creditCard.setNumber(payload.getCardNumber());

        // Find owner of the credit card

        int userID = payload.getUserId();

        if (!userRepository.existsById(userID))
        {
            return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
        }
        creditCard.setOwner(userRepository.getReferenceById(userID));

        // Save the instance to database
        creditCardRepository.save(creditCard);
       
        // Return HTTP response
        return new ResponseEntity<>(creditCard.getId(), HttpStatus.OK);
       
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null

        List<CreditCardView> creditCardViews = new ArrayList<>();
        if (!userRepository.existsById(userId)) 
        {
            return new ResponseEntity<>(creditCardViews, HttpStatus.BAD_REQUEST);
        }
        User currUser = userRepository.getReferenceById(userId);
        List<CreditCard> creditCards = currUser.getCreditCards();       
        for (CreditCard creditCard : creditCards) {
            creditCardViews.add( new CreditCardView(creditCard.getIssuanceBank(),creditCard.getNumber()));         
        }

        return new ResponseEntity<>(creditCardViews, HttpStatus.OK);

    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
      
        // Find instance of credit card associated with credi card number
        CreditCard currCreditCard = creditCardRepository.findByNumber(creditCardNumber);
        if (currCreditCard == null) 
        {
            return new ResponseEntity<>(0, HttpStatus.BAD_REQUEST);
        }

        // Find owner of the credit card

        List<User> allUsers = userRepository.findAll();

        for (User user : allUsers) {
            if(user.getCreditCards().contains(currCreditCard))
            {
                return new ResponseEntity<>(user.getId(), HttpStatus.OK);

            }
        }

        // Return Id of the owner

        return new ResponseEntity<>(currCreditCard.getId(), HttpStatus.BAD_REQUEST);
    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<Integer> updateCreditCardBalance(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.

        // Find instance of credit card associated with credit card number
       

        // Update their balance history
        for (UpdateBalancePayload updateBalancePayload : payload) {

            // Find associated credit card for transaction
            CreditCard currCreditCard = creditCardRepository.findByNumber(updateBalancePayload.getCreditCardNumber());
            TreeMap<LocalDate, BalanceHistory> currBalanceHistoryMap = new TreeMap<>();
            currBalanceHistoryMap.putAll(currCreditCard.getBalanceHistory());

            // Deconstruct the payload 
            LocalDate currDate = updateBalancePayload.getBalanceDate();
            Double currAmount = updateBalancePayload.getBalanceAmount();

            // Find neighbouring values for transaction
            Double prevAmount = currBalanceHistoryMap.lowerEntry(currDate).getValue().getBalance();
            Double nextAmount = currBalanceHistoryMap.higherEntry(currDate).getValue().getBalance();

      
            // Update transaction in case of missing values
            nextAmount = nextAmount + (currAmount - prevAmount);

            BalanceHistory nextBalanceHistory = new BalanceHistory();
            nextBalanceHistory.setBalance(nextAmount);
            nextBalanceHistory.setDate(currBalanceHistoryMap.higherKey(currDate));
            nextBalanceHistory.setCreditCard(currBalanceHistoryMap.higherEntry(currDate).getValue().getCreditCard());

            currBalanceHistoryMap.put(currBalanceHistoryMap.higherKey(currDate), nextBalanceHistory);


            BalanceHistory balanceHistory = new BalanceHistory();
            balanceHistory.setBalance(currAmount);
            balanceHistory.setDate(currDate);
            balanceHistory.setCreditCard(currCreditCard);
        
            currBalanceHistoryMap.put(updateBalancePayload.getBalanceDate(), balanceHistory);

            balanceHistoryRepository.save(balanceHistory);

            currCreditCard.setBalanceHistory(currBalanceHistoryMap);

        
        }


        return new ResponseEntity<>(1, HttpStatus.OK);

    }

    @GetMapping("/credit-card:history")
    public ResponseEntity<Map<LocalDate,BalanceHistory>> getBalanceHistoryForCreditCard(@RequestParam String creditCardNumber) {
   
        // Find instance of credit card associated with credi card number
        CreditCard currCreditCard = creditCardRepository.findByNumber(creditCardNumber);

        // Get  balance history

        return new ResponseEntity<>(currCreditCard.getBalanceHistory(), HttpStatus.OK);    }


}
    

