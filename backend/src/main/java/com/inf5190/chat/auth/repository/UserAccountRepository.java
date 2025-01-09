package com.inf5190.chat.auth.repository;

import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;

@Repository
public class UserAccountRepository {
    private static final String COLLECTION_NAME = "userAccounts";
    private final Firestore firestore;


    public UserAccountRepository(Firestore firestore){
        this.firestore = firestore;
    }

    /**
    * SOURCE : https://firebase.google.com/docs/firestore/query-data/get-data
    */
    // appelle fait une requete a notre base de donnes pour obtenir un compte a partir d'un nom d'utilisateur
    public FirestoreUserAccount getUserAccount(String username) throws InterruptedException, ExecutionException {
        
        try{

            DocumentReference doc = firestore.collection(COLLECTION_NAME).document(username);
            // on passe de doc a ApiFuture a Document snapshot, 2 appel blockant
            DocumentSnapshot resp  = doc.get().get();

            if(resp.exists()){
                return resp.toObject(FirestoreUserAccount.class);
            }else{
                return null;
                //throw new UnsupportedOperationException("Account doesn't exist.");
            }

        // Si on ne rcois rien ou si un thread 
        } catch (InterruptedException | ExecutionException e) {
            throw new UnsupportedOperationException("Couldn't get account from firestore database. username : " + username , e);
        }   
    }

    // fait une requete pour creer un nouveau compte
    public void createUserAccount(FirestoreUserAccount userAccount) {
      
        try {

            // On verifie si les utilisateurs existe deja
            if (getUserAccount(userAccount.getUsername()) == null) {
                ApiFuture<WriteResult> wf = firestore.collection(COLLECTION_NAME)
                                                              .document(userAccount.getUsername())
                                                              .set(userAccount);
                try {
                    WriteResult wr = wf.get();    // On fais un get bidon pour attaper les erreurs

                // On arrive pas attrape
                } catch (InterruptedException | ExecutionException e) {

                    throw new RuntimeException("Error while writing to Firestore, couldn't confirm write", e);
                }

            } else {
                throw new UnsupportedOperationException("Account with the same username already exists.");
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Error checking if user account exists", e);
        }
    }
}