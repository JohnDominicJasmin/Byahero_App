package FirebaseUserManager;



import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;



public class FirebaseManager {


    private static FirebaseAuth firebaseAuth;
    private static FirebaseUser firebaseUser;


    public static void getCreatedUserAccount() {

        firebaseUser = firebaseAuth.getCurrentUser();
    }

    public static FirebaseUser getFirebaseUserInstance(){

        return firebaseUser;
    }
    public static FirebaseAuth getFirebaseAuthInstance() {

        return firebaseAuth;
    }

    public static void initializeFirebaseApp() {

        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static boolean hasAccountSignedIn() {

        return firebaseUser != null;
    }



}
