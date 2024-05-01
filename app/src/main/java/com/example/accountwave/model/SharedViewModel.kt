import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.Dispatchers

class SharedViewModel : ViewModel() {
    private val _transactionAdded = MutableLiveData<Boolean>()
    val transactionAdded: LiveData<Boolean> = _transactionAdded

    suspend fun onTransactionAdded() {
        kotlinx.coroutines.withContext(Dispatchers.Main) {
        _transactionAdded.value = true
    }}

    fun resetTransactionAdded() {
        _transactionAdded.value = false
    }
}