package com.inf5190.chat.messages.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import com.inf5190.chat.messages.model.ChatImageData;
import com.inf5190.chat.messages.model.NewMessageRequest;
import io.jsonwebtoken.io.Decoders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.inf5190.chat.messages.model.Message;
import com.google.cloud.Timestamp;

/**
 * Classe qui gère la persistence des messages.
 */
@Repository
public class MessageRepository {

    private final Firestore firestore;
    private final StorageClient storageClient;

    @Autowired
    @Qualifier("firebaseBucketName")
    private String storageBucketName;

    private final String COLLECTION_NAME = "messages";
    private Timestamp timestamp;

    // Injection via constructeur pour Firestore et StorageClient
    public MessageRepository(Firestore firestore, StorageClient storageClient) {
        this.firestore = firestore;
        this.storageClient = storageClient;
    }

    public List<Message> getMessages(String fromId) {
        List<Message> unseenMessages = new ArrayList<>();

        try {
            Query doc;

            if (fromId == null || fromId.isEmpty()) {
                doc = firestore.collection(COLLECTION_NAME)
                    .orderBy("timestamp", Query.Direction.ASCENDING) 
                    .limitToLast(20);
            } else {
                DocumentSnapshot docStart = firestore.collection(COLLECTION_NAME)
                                                      .document(fromId)
                                                      .get()
                                                      .get();
                doc = firestore.collection(COLLECTION_NAME)
                    .orderBy("timestamp", Query.Direction.ASCENDING) 
                    .startAfter(docStart);
            }

            ApiFuture<QuerySnapshot> docResp = doc.get();
            List<QueryDocumentSnapshot> documents = docResp.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                String username = document.getString("username"); 
                Timestamp ts = document.getTimestamp("timestamp"); 
                String text = document.getString("text");
                String imageUrl = document.getString("imageUrl");

                Message msgReceived = new Message(
                    document.getId(),  
                    username,          
                    ts.getSeconds()*1000,         
                    text,
                    imageUrl);
                
                unseenMessages.add(msgReceived); 
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Couldn't retrieve messages from database", e);
        }
        return unseenMessages;
    }

    public Message createMessage(NewMessageRequest message) {
        String user = message.username();
        String content = message.text();
        timestamp = Timestamp.now();

        FirestoreMessage msg = new FirestoreMessage(user, timestamp, content, null);

        try {
            ApiFuture<DocumentReference> newMsg = firestore.collection(COLLECTION_NAME).add(msg);
            DocumentReference doc = newMsg.get();
            String firestoreMsgId = doc.getId();

            String imageUrl = null;
            if (message.imageData() != null) {
                imageUrl = storeImageInBucket(message.imageData(), firestoreMsgId);

                Map<String, Object> updates = new HashMap<>();
                updates.put("imageUrl", imageUrl);

                doc.update(updates).get();
            }

            return new Message(firestoreMsgId, user, timestamp.getSeconds(), content, imageUrl);

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Couldn't send message to database", e);
        }
    }

    private String storeImageInBucket(ChatImageData imageData, String messageID) {
        Bucket b = StorageClient.getInstance().bucket(storageBucketName);
        String path = String.format("images/%s.%s", messageID, imageData.type());
        b.create(path, Decoders.BASE64.decode(imageData.data()),
                Bucket.BlobTargetOption.predefinedAcl(Storage.PredefinedAcl.PUBLIC_READ));
        
        System.out.println(storageBucketName);
        return String.format("https://storage.googleapis.com/%s/%s", storageBucketName, path);
    }

}
